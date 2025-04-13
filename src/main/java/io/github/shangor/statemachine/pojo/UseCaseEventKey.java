package io.github.shangor.statemachine.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UseCaseEventKey {
    private String useCaseId;
    private String fromState;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UseCaseEventKey that = (UseCaseEventKey) obj;
        return useCaseId.equals( that.useCaseId) && fromState.equals(that.fromState);
    }

    @Override
    public String toString() {
        return "%s.%s".formatted(useCaseId, fromState);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public static UseCaseEventKey fromString(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        var separatorIndex = key.indexOf(".");
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Invalid key format");
        } else if (separatorIndex == key.length() - 1) {
            throw new IllegalArgumentException("Invalid key format");
        }

        UseCaseEventKey useCaseEventKey = new UseCaseEventKey();
        useCaseEventKey.setUseCaseId(key.substring(0, separatorIndex));
        useCaseEventKey.setFromState(key.substring(separatorIndex + 1));
        return useCaseEventKey;
    }
}
