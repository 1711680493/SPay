package shendi.pay.bean;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import shendi.pay.util.AccessibilityNodeUtil;

/**
 * 对 AccessibilityNodeInfo 的包装
 * 创建时间：2024/7/15
 * @author 砷碲
 */
public class ANodeInfo {

    public AccessibilityNodeInfo node;

    /** 当前节点在父结点的位置, -1为无父节点, -2还未初始化 */
    private int nodeIndex = -2;

    public ANodeInfo(AccessibilityNodeInfo node) {
        this.node = node;
    }
    public ANodeInfo(AccessibilityNodeInfo node, int nodeIndex) {
        this.node = node;
        this.nodeIndex = nodeIndex;
    }

    /**
     * 获取当前节点在父结点的位置，-1代表没有父节点
     * @return 位置
     */
    public int getNodeIndex() {
        if (nodeIndex == -2) {
            nodeIndex = AccessibilityNodeUtil.parentIndex(node);
        }
        return nodeIndex;
    }

    public ANodeInfo getParent() {
        return new ANodeInfo(node.getParent());
    }

    /** 获取子节点，如果包装的节点为空则继续返回一个包装空节点的对象 */
    public ANodeInfo getChild(int index) {
        return getChild(index, null);
    }

    /** 获取子节点，如果包装的节点为空或节点的类名称与传递的不一致则继续返回一个包装空节点的对象，类名称为空则忽略 */
    public ANodeInfo getChild(int index, String className) {
        if (node == null) return new ANodeInfo(null, index);

        AccessibilityNodeInfo childNode = node.getChild(index);
        if (childNode != null && className != null && !childNode.getClassName().equals(className)) {
            childNode = null;
        }

        return new ANodeInfo(childNode, index);
    }

    /** 获取子节点并将当前节点回收，如果包装的节点为空则继续返回一个包装空节点的对象 */
    public ANodeInfo getChildAndRecycle(int index) {
        return getChildAndRecycle(index, null);
    }

    /** 获取子节点并将当前节点回收，如果包装的节点为空或节点的类名称与传递的不一致则继续返回一个包装空节点的对象，类名称为空则忽略 */
    public ANodeInfo getChildAndRecycle(int index, String className) {
        if (node == null) return new ANodeInfo(null, index);

        AccessibilityNodeInfo childNode = node.getChild(index);
        if (childNode != null && className != null && !childNode.getClassName().equals(className)) {
            childNode = null;
        }

        node.recycle();
        return new ANodeInfo(childNode, index);
    }

    public List<ANodeInfo> findNodeByText(String text) {
        return AccessibilityNodeUtil.findNodeByText(node, text);
    }

    public String getText() {
        if (node.getText() != null) return node.getText().toString();
        return null;
    }

    public String getContentDescription() {
        if (node.getContentDescription() != null) return node.getContentDescription().toString();
        return null;
    }

    public boolean performAction(int action) {
        return node.performAction(action);
    }

    public String getClassName() {
        if (node.getClassName() != null) return node.getClassName().toString();
        return null;
    }

    public boolean isClickable() {
        return node.isClickable();
    }

    public int getChildCount() {
        return node.getChildCount();
    }

    public void recycle() {
        node.recycle();
    }

}
