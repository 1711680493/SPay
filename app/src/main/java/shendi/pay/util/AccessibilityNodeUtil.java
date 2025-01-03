package shendi.pay.util;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

import shendi.kit.util.StringUtil;
import shendi.pay.bean.ANodeInfo;

/**
 * 关于无障碍服务信息节点的工具类.
 * 创建时间：2024/7/9
 * @author 砷碲
 */
public class AccessibilityNodeUtil {

    /**
     * 打印节点信息及子节点
     * @param nodeInfo 节点
     */
    public static void printTree(AccessibilityNodeInfo nodeInfo) {
        printTree(nodeInfo, 0);
    }
    /**
     * 打印节点信息及子节点
     * @param nodeInfo 节点
     */
    public static void printTree(ANodeInfo nodeInfo) {
        printTree(nodeInfo, 0);
    }

    /**
     * 打印节点信息及子节点
     * @param nodeInfo  节点,不会调用recycle释放
     * @param level     层级
     */
    public static void printTree(AccessibilityNodeInfo nodeInfo, int level) {
        if (nodeInfo == null) return;

        StringBuilder infoStr = new StringBuilder();
        infoStr.append(StringUtil.repeat("\t", level)).append("|-[")
//                .append(nodeInfo.getPackageName()).append(" | ")
                .append(nodeInfo.getClassName()).append("]")
                .append("(").append(nodeInfo.getText()).append(")(").append(nodeInfo.getContentDescription()).append(")[");

        // 位置信息,x,y就是点击中心
        Rect rect = new Rect();
        nodeInfo.getBoundsInScreen(rect);

        infoStr.append(rect.centerX()).append(",").append(rect.centerY()).append("](")
                .append((rect.right - rect.left)).append(",").append((rect.bottom - rect.top)).append(")");

        infoStr.append("[").append(nodeInfo.isClickable() ? "可点击" : "不可点击")
//                .append(" | ").append(nodeInfo.isContextClickable() ? "可c点击" : "不可c点击")
//                .append(" | ").append(nodeInfo.isLongClickable() ? "可l点击" : "不可l点击")
                .append(" | ").append(nodeInfo.isVisibleToUser() ? "可见" : "不可见")
                .append(" | ").append(nodeInfo.isEditable() ? "可编辑" : "不可编辑")
                .append("]");

        System.out.println(infoStr);

        int nextLevel = level + 1;
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo child = nodeInfo.getChild(i);
            if (child == null) continue;

            printTree(child, nextLevel);
            child.recycle();
        }
    }
    /**
     * 打印节点信息及子节点
     * @param nodeInfo  节点,不会调用recycle释放
     * @param level     层级
     */
    public static void printTree(ANodeInfo nodeInfo, int level) {
        printTree(nodeInfo.node, level);
    }


    /**
     * 打印节点信息,json格式
     * @param nodeInfo  节点
     * @return 节点转的json，可用于后续处理
     */
    public static JSONObject print(AccessibilityNodeInfo nodeInfo) {
        JSONObject obj = nodeToJson(nodeInfo);
        String str = obj.toString();

        int count = 3000, len = str.length();
        int num = len / count + 1;

        for (int i = 0; i < num; i++) {
            int curSize = len > count ? count : len;
            len -= curSize;

            int beginIndex = i*count;
            System.out.println(str.substring(beginIndex, beginIndex + curSize));
        }

        return obj;
    }

    /**
     * 将节点转为json信息
     * @param nodeInfo  节点
     * @return 当前节点的JSON信息
     */
    public static JSONObject nodeToJson(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return new JSONObject();

        JSONObject obj = new JSONObject();
        obj.put("packName", nodeInfo.getPackageName());
        obj.put("className", nodeInfo.getClassName());
        obj.put("text", nodeInfo.getText());
        obj.put("isClick", nodeInfo.isClickable());
        obj.put("isVisible", nodeInfo.isVisibleToUser());
        obj.put("isEdit", nodeInfo.isEditable());

        // 位置信息,x,y就是点击中心
        Rect rect = new Rect();
        nodeInfo.getBoundsInScreen(rect);

        obj.put("centerX", rect.centerX());
        obj.put("centerY", rect.centerY());
        obj.put("left", rect.left);
        obj.put("right", rect.right);
        obj.put("top", rect.top);
        obj.put("bottom", rect.bottom);
        obj.put("width", rect.right - rect.left);
        obj.put("height", rect.bottom - rect.top);

        JSONArray children = new JSONArray();
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            children.add(nodeToJson(nodeInfo.getChild(i)));
        }
        obj.put("children", children);

        nodeInfo.recycle();

        return obj;
    }

    /**
     * 从指定节点中找到指定文本的节点列表.
     * @param nodeInfo 节点
     * @param text 查找的文本节点
     * @return 节点列表
     */
    public static List<ANodeInfo> findNodeByText(AccessibilityNodeInfo nodeInfo, String text) {
        return findNodeByText(nodeInfo, text, new ArrayList<>());
    }
    /**
     * 从指定节点中找到指定文本的节点列表.
     * @param nodeInfo 节点
     * @param text 查找的文本节点
     * @param list 用于存储指定文本的列表
     * @return 节点列表
     */
    public static List<ANodeInfo> findNodeByText(AccessibilityNodeInfo nodeInfo, String text, List<ANodeInfo> list) {
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
            CharSequence tmpText = childNode.getText();
            boolean isHasText = false;
            if (tmpText != null) {
                String childText = tmpText.toString();
                isHasText = childText.indexOf(text) != -1;
                if (isHasText) {
                    list.add(new ANodeInfo(childNode, i));
                }
            }

            findNodeByText(childNode, text, list);
            if (!isHasText) {
                childNode.recycle();
            }
        }
        return list;
    }

    /**
     * 获取当前节点在父节点中的位置.
     * @param nodeInfo 节点
     * @return 父节点中的位置，-1为无父节点
     */
    public static int parentIndex(AccessibilityNodeInfo nodeInfo) {
        AccessibilityNodeInfo parentNode = nodeInfo.getParent();
        int index = -1;

        if (parentNode != null) {
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                // 每次get都是新的对象
                AccessibilityNodeInfo ni = parentNode.getChild(i);
                if (ni.getClassName().equals(nodeInfo.getClassName()) && ni.getChildCount() == nodeInfo.getChildCount() && ni.isClickable() == nodeInfo.isClickable()
                    && (ni.getText() == nodeInfo.getText() || ni.getText().equals(nodeInfo.getText()))) {
                    index = i;
                }
                ni.recycle();

                if (index != -1) break;
            }
            parentNode.recycle();
        }

        return index;
    }

}
