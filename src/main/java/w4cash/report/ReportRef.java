package w4cash.report;

import java.util.List;

public record ReportRef(String name, List<ReportParamInfo> parameters) {
}
