#!/bin/sh
set -eu

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"

echo "Waiting for Kafka at ${BOOTSTRAP}..."
until /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; do
  sleep 2
done

create_topic() {
  topic="$1"
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists \
    --topic "${topic}" \
    --partitions 3 \
    --replication-factor 1
  echo "Ensured topic ${topic}"
}

for topic in \
  identity.user.changed \
  identity.doctor.changed \
  booking.appointment.changed \
  booking.waitlist.changed \
  audit.event.recorded
do
  create_topic "${topic}"
  create_topic "${topic}.DLT"
done

echo "Kafka topics ready."
