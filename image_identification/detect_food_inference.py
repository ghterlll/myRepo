#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Minimal food detection with YOLOv8 (COCO weights)
- Filters common food classes
- Outputs:
  1) results.json: per-image per-class max confidence (deduped)
  2) summary.json: per-image class counts
"""

import argparse
import json
import os
from pathlib import Path
from typing import List, Dict, Any

import cv2
from ultralytics import YOLO

import csv

def load_calorie_map(path="calorie_map.csv"):
    m = {}
    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        # CSV columns: label,kcal_100g
        for row in reader:
            label = (row.get("label") or "").strip()
            kcal = row.get("kcal_100g")
            if label and kcal:
                try:
                    m[label] = float(kcal)
                except:
                    pass
    return m

CALORIE_MAP = load_calorie_map("calorie_map.csv")

# Common food classes (editable)
FOODY_LABELS = {
    "apple", "banana", "orange", "carrot", "broccoli",
    "pizza", "hot dog", "donut", "cake", "sandwich"
    #"bowl", "cup", "bottle", "wine glass",
    #"knife", "fork", "spoon"  # optional tableware
}

CALORIE_MAP = {
    "apple": 52,        # kcal / 100g
    "banana": 89,
    "orange": 47,
    "sandwich": 250,    # approx
    "broccoli": 34,
    "carrot": 41,
    "hot dog": 290,
    "pizza": 266,
    "donut": 452,
    "cake": 350
}

def parse_args():
    ap = argparse.ArgumentParser(description="YOLOv8 食材识别（推理-only）")
    ap.add_argument("--source", required=True, help="图片/文件夹/视频/摄像头。例：meal.jpg 或 ./images")
    ap.add_argument("--weights", default="yolov8n.pt", help="预训练权重（默认 yolov8n.pt）")
    ap.add_argument("--conf", type=float, default=0.30, help="置信度阈值")
    ap.add_argument("--imgsz", type=int, default=640, help="推理分辨率短边")
    ap.add_argument("--device", default=None, help="指定设备：cpu/cuda:0/mps(Apple)")
    ap.add_argument("--save", action="store_true", help="保存带框可视化图片")
    ap.add_argument("--save_json", default="results.json", help="检测结果 JSON 文件名")
    ap.add_argument("--project", default="runs/food_det", help="输出目录")
    ap.add_argument("--name", default="exp", help="子目录名")
    return ap.parse_args()


def results_to_dicts(res, keep_labels: set, names: Dict[int, str], conf_thr: float) -> List[Dict[str, Any]]:
    """Convert YOLO results to list[dict]."""
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
    """Draw detection boxes on image."""
    for d in dets:
        x1, y1, x2, y2 = map(int, d["box_xyxy"])
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 180, 0), 2)
        text = f'{d["label"]} {d["conf"]:.2f}'
        (tw, th), _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 0.6, 2)
        cv2.rectangle(img, (x1, y1 - th - 8), (x1 + tw + 6, y1), (0, 180, 0), -1)
        cv2.putText(img, text, (x1 + 3, y1 - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)


def main():
    args = parse_args()
    os.makedirs(args.project, exist_ok=True)

    model = YOLO(args.weights)
    results = model.predict(
        source=args.source,
        conf=args.conf,
        imgsz=args.imgsz,
        device=args.device,
        save=args.save,
        project=args.project,
        name=args.name,
        save_json=False,  # keep only custom JSON
        verbose=False
    )

    names = model.names
    final_report: Dict[str, List[Dict[str, Any]]] = {}
    summary_report: Dict[str, Dict[str, int]] = {}

    # Iterate frames/images
    for i, res in enumerate(results):
        in_path = Path(getattr(res, "path", f"frame_{i:06d}.jpg"))
        key = str(in_path.name)

        dets = results_to_dicts(res, FOODY_LABELS, names, conf_thr=args.conf)

        # Deduplicate: keep highest confidence per class
        unique = {}
        for d in dets:
            label = d["label"]
            if label not in unique or d["conf"] > unique[label]["conf"]:
                unique[label] = d
        dets_unique = list(unique.values())
        final_report[key] = dets_unique
        for d in dets:
            d["kcal_100g"] = CALORIE_MAP.get(d["label"], None)

        # Count occurrences per class
        counts = {}
        for d in dets:
            counts[d["label"]] = counts.get(d["label"], 0) + 1
        summary_report[key] = counts

        # Visualization
        if args.save and hasattr(res, "orig_img") and res.orig_img is not None:
            img = res.orig_img.copy()
            draw_boxes(img, dets_unique)
            out_dir = Path(args.project) / args.name / "vis"
            out_dir.mkdir(parents=True, exist_ok=True)
            cv2.imwrite(str(out_dir / key), img)

    # Save results.json
    if args.save_json:
        result_path = Path(args.project) / args.name / args.save_json
        result_path.parent.mkdir(parents=True, exist_ok=True)
        with open(result_path, "w", encoding="utf-8") as f:
            json.dump(final_report, f, ensure_ascii=False, indent=2)
        print(f"[OK] 去重检测结果已保存: {result_path}")

    # Save summary.json
    summary_path = Path(args.project) / args.name / "summary.json"
    with open(summary_path, "w", encoding="utf-8") as f:
        json.dump(summary_report, f, ensure_ascii=False, indent=2)

    print(json.dumps(final_report, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
