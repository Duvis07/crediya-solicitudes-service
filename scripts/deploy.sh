#!/bin/bash

# CrediYa Solicitudes Service - Deploy Script for Docker
# This script manages deployment operations for the solicitudes service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="docker-compose.yml"
SERVICE_NAME="solicitudes-service"

# Function to display usage
usage() {
    echo -e "${BLUE}Usage: $0 {up|down|restart|logs|status|build}${NC}"
    echo -e "${YELLOW}Commands:${NC}"
    echo -e "  up      - Start all services"
    echo -e "  down    - Stop and remove all services"
    echo -e "  restart - Restart all services"
    echo -e "  logs    - Show service logs"
    echo -e "  status  - Show service status"
    echo -e "  build   - Build and start services"
    exit 1
}

# Function to check if docker-compose is available
check_podman_compose() {
    if ! command -v podman-compose &> /dev/null; then
        echo -e "${RED}podman-compose not found. Installing...${NC}"
        pip3 install podman-compose
    fi
}

# Function to start services
start_services() {
    echo -e "${BLUE}Starting CrediYa Solicitudes Service...${NC}"
    check_podman_compose
    podman-compose -f $COMPOSE_FILE up -d
    echo -e "${GREEN}Services started successfully${NC}"
    echo -e "${YELLOW}Service URLs:${NC}"
    echo -e "   • Solicitudes API: ${BLUE}http://localhost:8081${NC}"
    echo -e "   • Health Check: ${BLUE}http://localhost:8081/actuator/health${NC}"
    echo -e "   • Swagger UI: ${BLUE}http://localhost:8081/swagger-ui.html${NC}"
}

# Function to stop services
stop_services() {
    echo -e "${BLUE}Stopping CrediYa Solicitudes Service...${NC}"
    check_podman_compose
    podman-compose -f $COMPOSE_FILE down
    echo -e "${GREEN}Services stopped successfully${NC}"
}

# Function to restart services
restart_services() {
    echo -e "${BLUE}Restarting CrediYa Solicitudes Service...${NC}"
    stop_services
    start_services
}

# Function to show logs
show_logs() {
    echo -e "${BLUE}Showing service logs...${NC}"
    check_podman_compose
    podman-compose -f $COMPOSE_FILE logs -f
}

# Function to show status
show_status() {
    echo -e "${BLUE}Service Status:${NC}"
    check_podman_compose
    podman-compose -f $COMPOSE_FILE ps
    echo ""
    echo -e "${BLUE}Docker Images:${NC}"
    podman images | grep -E "(crediya|postgres)" || echo "No CrediYa images found"
}

# Function to build and start
build_and_start() {
    echo -e "${BLUE}Building and starting services...${NC}"
    ./scripts/build.sh
    start_services
}

# Main script logic
case "${1:-}" in
    up)
        start_services
        ;;
    down)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    logs)
        show_logs
        ;;
    status)
        show_status
        ;;
    build)
        build_and_start
        ;;
    *)
        usage
        ;;
esac
