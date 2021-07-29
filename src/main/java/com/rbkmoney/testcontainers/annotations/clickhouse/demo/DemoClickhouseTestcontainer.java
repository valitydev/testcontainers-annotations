package com.rbkmoney.testcontainers.annotations.clickhouse.demo;

import com.rbkmoney.testcontainers.annotations.clickhouse.ClickhouseTestcontainer;

/**
 * this is a demo example of filling in annotation, do not use
 */
@ClickhouseTestcontainer(
        properties = "clickhouse.db.connection.timeout=60000",
        migrations = {
                "sql/db_init.sql",
                "sql/V2__create_events_p2p.sql",
                "sql/V3__create_fraud_payments.sql",
                "sql/V4__create_payment.sql",
                "sql/V5__add_fields.sql",
                "sql/V6__add_result_fields_payment.sql",
                "sql/V7__add_fields.sql"})
public @interface DemoClickhouseTestcontainer {
}
