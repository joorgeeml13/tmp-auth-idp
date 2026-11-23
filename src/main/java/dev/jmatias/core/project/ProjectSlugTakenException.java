package dev.jmatias.core.project;

import dev.jmatias.shared.exception.CustomException;

public class ProjectSlugTakenException extends CustomException {

    public ProjectSlugTakenException(String slug) {
        super(409, "project.slug.taken", slug);
    }
}
