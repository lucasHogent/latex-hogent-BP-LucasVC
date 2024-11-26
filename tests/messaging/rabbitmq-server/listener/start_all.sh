#!/bin/sh
# Path to the text file containing the values
TEXT_FILE="/opt/jprogram/listeners.txt"

# Path to the start_listener script
START_SCRIPT="/opt/jprogram/start_listener.sh"

# Log file for the execution
LOG_FILE="/opt/jprogram/listeners.log"

# Print a log message indicating the script is starting
echo "$(date) - Starting Listener Services..." >> "$LOG_FILE"

# Check if the text file exists
if [[ ! -f "$TEXT_FILE" ]]; then
    echo "$(date) - Error: Text file $TEXT_FILE not found!" >> "$LOG_FILE"
    exit 1
fi

# Iterate through each line in the text file
while IFS= read -r line || [[ -n "$line" ]]; do
    # Skip empty lines
    if [[ -z "$line" ]]; then
        continue
    fi

    # Log the start of the listener
    echo "$(date) - Starting listener with argument: $line" >> "$LOG_FILE"

    # Run the start_listener.sh script with the current value
    "$START_SCRIPT" "$line" >> "$LOG_FILE" 2>&1 &

    # Log the PID of the started process
    echo "$(date) - Listener started with argument: $line, PID: $!" >> "$LOG_FILE"

done < "$TEXT_FILE"

# Print a message indicating all listeners have been started
echo "$(date) - All listeners started." >> "$LOG_FILE"
