package su.syel.fourthrest.mapper;

import org.mapstruct.Mapper;
import su.syel.fourthrest.dto.response.DocumentResponse;
import su.syel.fourthrest.model.DocumentEntity;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentResponse toResponseDTO(DocumentEntity entity);
}
