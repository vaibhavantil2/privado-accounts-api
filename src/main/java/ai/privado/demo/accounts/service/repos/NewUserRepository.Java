package ai.privado.demo.accounts.service.repos;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import ai.privado.demo.accounts.service.entity.newusere;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    @Autowired
    private DynamoDBMapper mapper;

    public Optional<newusere> findById(String id) {
        return Optional.ofNullable(mapper.load(UserE.class, id));
    }

    public void save(newusere user) {
        mapper.save(user);
    }

    public void delete(newusere user) {
        mapper.delete(user);
    }

    public Optional<newusere> findByEmail(String email) {
        // Assuming 'email' is a secondary index or part of primary key
        newusere userE = new UserE();
        userE.setEmail(email); // set the email on an empty UserE object

        DynamoDBQueryExpression<UserE> queryExpression = new DynamoDBQueryExpression<UserE>()
                .withHashKeyValues(userE);

        PaginatedQueryList<UserE> result = mapper.query(UserE.class, queryExpression);

        if (!result.isEmpty()) {
            // Assuming email is unique and only one record will be returned
            return Optional.of(result.get(0));
        }

        return Optional.empty();
    }

    // Additional methods...
}
