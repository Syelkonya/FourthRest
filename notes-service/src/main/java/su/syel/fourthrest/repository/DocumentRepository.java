package su.syel.fourthrest.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import su.syel.fourthrest.model.DocumentEntity;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentEntity, String> {
}
