#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
YOLOv8 食材/食物演示级识别（纯推理）
- 本地预训练权重（COCO）
- 过滤常见食物/餐具类目
- 输出：带框图片 + JSON（每张图的识别结果）
"""

import argparse
import json
import os
from pathlib import Path
from typing import List, Dict, Any

import cv2
from ultralytics import YOLO


# COCO 中与“食物/餐具/餐饮相关”的类目（可按需增删）
FOODY_LABELS = {
    "apple", "banana", "orange", "carrot", "broccoli",
    "pizza", "hot dog", "donut", "cake", "sandwich",
    "bowl", "cup", "bottle", "wine glass",
    "knife", "fork", "spoon"
}

def parse_args():
    ap = argparse.ArgumentParser(description="YOLOv8 食材识别（推理-only）")
    ap.add_argument("--source", required=True,
                    help="图片/文件夹/视频/摄像头(0、1…)。例：meal.jpg 或 ./images")
    ap.add_argument("--weights", default="yolov8n.pt",
                    help="预训练权重（默认最小 yolov8n.pt）")
    ap.add_argument("--conf", type=float, default=0.30,
                    help="置信度阈值，默认0.30")
    ap.add_argument("--imgsz", type=int, default=640,
                    help="推理分辨率（短边），默认640")
    ap.add_argument("--device", default=None,
                    help="指定设备，如 'cpu'、'cuda:0'、'mps'（Apple）")
    ap.add_argument("--save", action="store_true",
                    help="保存带框可视化图片/视频")
    ap.add_argument("--save_json", default="results.json",
                    help="保存检测结果 JSON 文件名；设为 '' 不落盘")
    ap.add_argument("--project", default="runs/food_det",
                    help="输出目录（图片/视频与缓存）")
    ap.add_argument("--name", default="exp",
                    help="子目录名（自动递增）")
    return ap.parse_args()


def is_image_file(p: Path) -> bool:
    return p.suffix.lower() in {".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp"}


def results_to_dicts(res, keep_labels: set, names: Dict[int, str], conf_thr: float) -> List[Dict[str, Any]]:
    out = []
    for b in res.boxes:
        cls_id = int(b.cls)
        label = names.get(cls_id, str(cls_id))
        conf = float(b.conf)
        if label in keep_labels and conf >= conf_thr:
            x1, y1, x2, y2 = [float(x) for x in b.xyxy[0].tolist()]
            out.append({
                "label": label,
                "conf": round(conf, 4),
                "box_xyxy": [round(x1, 1), round(y1, 1), round(x2, 1), round(y2, 1)]
            })
    return out


def draw_boxes(img, dets: List[Dict[str, Any]]):
    for d in dets:
        x1, y1, x2, y2 = map(int, d["box_xyxy"])
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 180, 0), 2)
        text = f'{d["label"]} {d["conf"]:.2f}'
        (tw, th), bs = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 0.6, 2)
        cv2.rectangle(img, (x1, y1 - th - 8), (x1 + tw + 6, y1), (0, 180, 0), -1)
        cv2.putText(img, text, (x1 + 3, y1 - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)


def main():
    args = parse_args()
    os.makedirs(args.project, exist_ok=True)

    model = YOLO(args.weights)
    # ultralytics 的 predict 会自己处理 source 是文件/文件夹/视频/摄像头
    # 我们自己再做一次后处理以过滤食物类目并保存 JSON

    # 运行推理
    results = model.predict(
        source=args.source,
        conf=args.conf,
        imgsz=args.imgsz,
        device=args.device,
        save=args.save,          # 若 True，会自动保存官方风格的可视化到 runs/... 里
        project=args.project,
        name=args.name,
        verbose=False
    )

    names = model.names
    final_report: Dict[str, List[Dict[str, Any]]] = {}

    # 遍历每一帧/每一张图片的结果
    for i, res in enumerate(results):
        # 结果中的路径属性：res.path
        in_path = Path(getattr(res, "path", f"frame_{i:06d}.jpg"))
        # 统一用相对短路径作为 key
        key = str(in_path.name)

        # 过滤成我们需要的“食物/餐具”子集
        dets = results_to_dicts(res, FOODY_LABELS, names, conf_thr=args.conf)
        final_report[key] = dets

        # 如果用户要求保存，但源是图片且我们想自行叠框（可选）
        # 注意：当 args.save=True 时，ultralytics 已经保存了渲染图，我们这里演示如何自绘并自存
        # 如果不想重复保存，可以注释掉下方块
        if args.save and hasattr(res, "orig_img") and res.orig_img is not None:
            img = res.orig_img.copy()
            draw_boxes(img, dets)
            out_dir = Path(args.project) / args.name / "vis"
            out_dir.mkdir(parents=True, exist_ok=True)
            cv2.imwrite(str(out_dir / key), img)

    # 保存 JSON
    if args.save_json:
        json_path = Path(args.project) / args.name / args.save_json
        json_path.parent.mkdir(parents=True, exist_ok=True)
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(final_report, f, ensure_ascii=False, indent=2)
        print(f"[OK] JSON saved to: {json_path}")

    # 同时把结果打印到控制台
    print(json.dumps(final_report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
