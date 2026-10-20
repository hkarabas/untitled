#!/bin/bash

# Script to generate a sample CSV file with 100,000 book records
# Format: title,author,price

OUTPUT_FILE="${1:-books_100000.csv}"
RECORD_COUNT="${2:-100000}"

# Arrays of common book elements
ADJECTIVES=("Great" "Amazing" "Wonderful" "Fantastic" "Incredible" "Mysterious" "Hidden" "Lost" "Ancient" "Modern" "Secret" "Dark" "Bright" "Silent" "Emerald" "Golden" "Silver" "Crystal" "Eternal" "Infinite")
NOUNS=("Adventure" "Journey" "Story" "Tale" "Mystery" "Dream" "Legend" "Quest" "Chronicle" "Saga" "Odyssey" "Treasure" "Kingdom" "Empire" "World" "Realm" "Sky" "Ocean" "Forest" "Castle")
FIRST_NAMES=("John" "Jane" "Michael" "Emily" "David" "Sarah" "Robert" "Lisa" "James" "Mary" "William" "Patricia" "Richard" "Jennifer" "Charles" "Linda" "Thomas" "Barbara" "Joseph" "Elizabeth")
LAST_NAMES=("Smith" "Johnson" "Williams" "Brown" "Jones" "Garcia" "Miller" "Davis" "Rodriguez" "Martinez" "Hernandez" "Lopez" "Gonzalez" "Wilson" "Anderson" "Thomas" "Taylor" "Moore" "Jackson" "Martin")

echo "Generating $RECORD_COUNT book records..."
> "$OUTPUT_FILE"

for ((i=1; i<=$RECORD_COUNT; i++)); do
    # Generate random title
    ADJ_IDX=$((RANDOM % ${#ADJECTIVES[@]}))
    NOUN_IDX=$((RANDOM % ${#NOUNS[@]}))
    TITLE="The ${ADJECTIVES[$ADJ_IDX]} ${NOUNS[$NOUN_IDX]} $i"

    # Generate random author
    FIRST_IDX=$((RANDOM % ${#FIRST_NAMES[@]}))
    LAST_IDX=$((RANDOM % ${#LAST_NAMES[@]}))
    AUTHOR="${FIRST_NAMES[$FIRST_IDX]} ${LAST_NAMES[$LAST_IDX]}"

    # Generate random price between 5.99 and 99.99
    PRICE=$(awk -v min=5.99 -v max=99.99 'BEGIN{print min+rand()*(max-min)}' | xargs printf "%.2f")

    # Write CSV line
    echo "\"$TITLE\",\"$AUTHOR\",$PRICE" >> "$OUTPUT_FILE"

    # Print progress every 10000 records
    if [ $((i % 10000)) -eq 0 ]; then
        echo "Generated $i records..."
    fi
done

echo "CSV file generated: $OUTPUT_FILE"
echo "Total records: $RECORD_COUNT"
wc -l "$OUTPUT_FILE"

