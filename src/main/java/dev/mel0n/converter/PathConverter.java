package dev.mel0n.converter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Class to convert Path to String and String to Path automatic
 */
@Converter(autoApply = true)
public class PathConverter implements AttributeConverter<List<Path>, String> {

    @Override
    public String convertToDatabaseColumn(List<Path> attribute) {
        return attribute == null ? null
                : attribute.stream()
                        .map(Path::toString)
                        .collect(Collectors.joining(";"));
    }

    @Override
    public List<Path> convertToEntityAttribute(String dbData) {

        return dbData != null ? null
                : Arrays.stream(dbData.split(";"))
                        .map(Path::of)
                        .toList();
    }
}
