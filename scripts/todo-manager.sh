#!/bin/bash

# Spring AI Agent TODO Manager Script
# Usage: ./scripts/todo-manager.sh [command] [options]

TODO_FILE="TODO.md"

show_help() {
    echo "Spring AI Agent TODO Manager"
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  list              Show all pending tasks"
    echo "  list-priority     Show tasks by priority"
    echo "  add \"<task>\"       Add a new task"
    echo "  complete <line>   Mark task as completed (by line number)"
    echo "  status            Show project status summary"
    echo "  help              Show this help"
    echo ""
}

list_tasks() {
    echo "📋 Pending Tasks:"
    echo "=================="
    grep -n "^\- \[ \]" "$TODO_FILE" | sed 's/^/  /'
    echo ""
    echo "✅ In Progress:"
    echo "==============="
    grep -n "^\- \[x\].*In Progress" "$TODO_FILE" | sed 's/^/  /'
    echo ""
}

list_by_priority() {
    echo "🔥 High Priority:"
    echo "=================="
    sed -n '/#### High Priority/,/#### Medium Priority/p' "$TODO_FILE" | grep "^\- \[ \]" | sed 's/^/  /'
    echo ""
    echo "⚡ Medium Priority:"
    echo "==================="
    sed -n '/#### Medium Priority/,/#### Low Priority/p' "$TODO_FILE" | grep "^\- \[ \]" | sed 's/^/  /'
    echo ""
    echo "📝 Low Priority:"
    echo "================"
    sed -n '/#### Low Priority/,/^$/p' "$TODO_FILE" | grep "^\- \[ \]" | sed 's/^/  /'
    echo ""
}

add_task() {
    local task="$1"
    if [ -z "$task" ]; then
        echo "❌ Please provide a task description"
        exit 1
    fi
    
    # Add to high priority section by default
    local temp_file=$(mktemp)
    awk -v task="- [ ] $task" '
        /#### High Priority/ { print; getline; print; print task; next }
        { print }
    ' "$TODO_FILE" > "$temp_file"
    
    mv "$temp_file" "$TODO_FILE"
    echo "✅ Task added to high priority section: $task"
}

show_status() {
    echo "📊 Spring AI Agent - Project Status"
    echo "===================================="
    
    local completed=$(grep -c "^\- \[x\]" "$TODO_FILE")
    local pending=$(grep -c "^\- \[ \]" "$TODO_FILE")
    local total=$((completed + pending))
    
    echo "Total Tasks: $total"
    echo "Completed: $completed"
    echo "Pending: $pending"
    
    if [ $total -gt 0 ]; then
        local percentage=$((completed * 100 / total))
        echo "Progress: $percentage%"
        
        # Simple progress bar
        local filled=$((percentage / 5))
        local empty=$((20 - filled))
        printf "Progress: ["
        printf "%*s" $filled | tr ' ' '█'
        printf "%*s" $empty | tr ' ' '░'
        printf "] $percentage%%\n"
    fi
    
    echo ""
    echo "Last Updated: $(date)"
}

# Main script
case "$1" in
    "list")
        list_tasks
        ;;
    "list-priority")
        list_by_priority
        ;;
    "add")
        add_task "$2"
        ;;
    "status")
        show_status
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        echo "❓ Unknown command: $1"
        show_help
        exit 1
        ;;
esac