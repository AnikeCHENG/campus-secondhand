package com.example.campussecondhand.enums;

/**
 * 举报处理状态。
 *
 * <p>0 待处理、1 已处理并处罚、2 已驳回。三态而非两态：
 * 驳回与处罚都必须留痕，否则管理员可以点一次就"处理完"，
 * 举报人与后续审计都无从分辨是查实了还是驳回了。</p>
 */
public enum ReportStatus {

    PENDING(0, "待处理"),
    HANDLED(1, "已处理并处罚"),
    REJECTED(2, "已驳回");

    private final int code;
    private final String label;

    ReportStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ReportStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReportStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }
}