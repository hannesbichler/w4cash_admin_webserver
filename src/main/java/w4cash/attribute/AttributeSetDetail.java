package w4cash.attribute;

import java.util.List;

public record AttributeSetDetail(String id, String name, List<AttributeUseRef> attributes) {
}
