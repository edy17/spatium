package org.diehl.spatium.infrastructure.aws.dynamodb;

import org.diehl.spatium.domain.model.Comment;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class CommentDynamoDbRepository implements AbstractDynamoDbRepository<Comment> {

    private static final String TABLE_NAME = "Comment";
    private static final String KEY_SCHEMA = "id";
    private static final List<String> columns = Stream.of(Comment.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toList());

    public ScanRequest scanByPostIdRequest(String postId, Map<String, AttributeValue> lastKey) {
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":val", AttributeValue.builder().s(postId).build());
        return ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("postId = :val")
                .expressionAttributeValues(expressionAttributeValues)
                .exclusiveStartKey(lastKey)
                .build();
    }

    @Override
    public PutItemRequest putRequest(Map<String, AttributeValue> item) {
        return PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public List<String> getColumns() {
        return columns;
    }

    @Override
    public String getKeySchema() {
        return KEY_SCHEMA;
    }
}
