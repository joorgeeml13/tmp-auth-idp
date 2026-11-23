package dev.jmatias.shared.tenant;

import jakarta.enterprise.context.RequestScoped;
import java.util.Optional;
import java.util.regex.Pattern;

/** Schema of the project the current request acts on. Set once per request, read by SchemaTenantResolver. */
@RequestScoped
public class TenantContext {

    // Same shape as core.project.schema_name. A schema name cannot be a bind parameter, so it must be vetted here.
    static final Pattern SCHEMA = Pattern.compile("p_[0-9a-f]{32}");

    private String schema;

    public void set(String schema) {
        if (schema == null || !SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("Invalid tenant schema name");
        }
        this.schema = schema;
    }

    public Optional<String> get() {
        return Optional.ofNullable(schema);
    }
}
