package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.utils.orders.CreationOrder;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.List;
import java.util.Set;

public interface EntityCreator<R extends TableRecord<R>> {
    R createAndGet(EntityDataPayload<R> data);
    <T> T createAndGet(EntityDataPayload<R> data, TableField<R,T> field);

    Set<TableField<R,?>> ignoreFieldsOnCreationOrder();

    default R consume(CreationOrder<R> order){
        R result = createAndGet(order.payload());

        List<CreationOrder.Mismatch<R>> mismatches = order.getMismatches(result, ignoreFieldsOnCreationOrder());
        if (!mismatches.isEmpty())
            throw new IllegalStateException(
                    """
                    Found mismatches while consuming order, add them to ignore fields if they're supposed to be different
                    Mismatches:
                    %s
                    """.formatted(mismatches)
            );

        return result;
    }
}
