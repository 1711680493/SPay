package shendi.pay.bean;

import shendi.kit.time.TimeUtils;

/**
 * 对应  notify_pay 表.
 * 创建时间：2023/11/9
 * @author Shendi
 */
public class NotifyPay {

    private long id;
    private String title;
    private String content;
    private String type;
    private long amount;
    private int state;
    private String reason;
    private long time;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        StringBuilder build = new StringBuilder();
        build.append("标题：").append(title).append("\n")
                .append("内容：").append(content).append("\n")
                .append("类型：").append(type).append("\n")
                .append("金额：").append(amount / 100f).append("\n")
                .append("状态：").append(state==0 ? "失败" : "成功").append("\n");
                if (state == 0) build.append("失败原因：").append(reason).append("\n");
                build.append("时间：").append(TimeUtils.getFormatTime().getString(time)).append("\n");
        return build.toString();
    }

}
