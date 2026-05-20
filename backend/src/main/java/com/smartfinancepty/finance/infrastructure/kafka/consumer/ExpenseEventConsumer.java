package com.smartfinancepty.finance.infrastructure.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.infrastructure.kafka.event.ExpenseCreatedEvent;
import com.smartfinancepty.finance.infrastructure.kafka.event.ExpenseDeletedEvent;
import com.smartfinancepty.finance.service.finance.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventConsumer {

    private final BudgetService budgetService;
    private final ObjectMapper objectMapper;

    /**
     * Al crear un gasto → verificar si se disparó alguna alerta de presupuesto
     */
    @KafkaListener(
            topics = "#{T(com.smartfinancepty.finance.infrastructure.kafka.event.ExpenseCreatedEvent).TOPIC}",
            groupId = "${spring.kafka.consumer.group-id:smartfinance-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onExpenseCreated(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            log.info("📥 Expense created event received: partition={}, offset={}",
                    record.partition(), record.offset());

            ExpenseCreatedEvent event =
                    objectMapper.convertValue(record.value(), ExpenseCreatedEvent.class);

            // Verificar alertas de presupuesto de forma asíncrona
            checkBudgetAlerts(event);

            ack.acknowledge();
            log.debug("✅ Expense event processed: userId={}, amount={}", event.getUserId(),
                    event.getAmount());

        } catch (Exception e) {
            log.error("❌ Error processing expense created event: {}", e.getMessage(), e);
            // No hacer ack → Kafka reintentará
        }
    }

    /**
     * Al eliminar un gasto → recalcular alertas de presupuesto
     */
    @KafkaListener(
            topics = "#{T(com.smartfinancepty.finance.infrastructure.kafka.event.ExpenseDeletedEvent).TOPIC}",
            groupId = "${spring.kafka.consumer.group-id:smartfinance-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onExpenseDeleted(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            log.info("📥 Expense deleted event received");
            ExpenseDeletedEvent event =
                    objectMapper.convertValue(record.value(), ExpenseDeletedEvent.class);

            log.info("🗑️ Gasto eliminado — userId={}, monto={}", event.getUserId(),
                    event.getAmount());
            ack.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing expense deleted event: {}", e.getMessage(), e);
        }
    }

    private void checkBudgetAlerts(ExpenseCreatedEvent event) {
        try {
            // Obtener presupuestos del mes del gasto y verificar alertas
            int year = event.getExpenseDate().getYear();
            int month = event.getExpenseDate().getMonthValue();

            var budgets = budgetService.getBudgetsByMonth(event.getUserId(), year, month);
            budgets.stream().filter(b -> b.isNearLimit() || b.isOverBudget())
                    .forEach(b -> log.warn("🔔 Budget alert: userId={}, category={}, usage={}%",
                            event.getUserId(), b.getCategoryName(), b.getUsagePercentage()));

        } catch (Exception e) {
            log.error("Error checking budget alerts: {}", e.getMessage());
        }
    }
}
