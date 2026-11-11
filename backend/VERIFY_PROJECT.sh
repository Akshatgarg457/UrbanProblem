#!/bin/bash

# Urban Problem Reporting System - Project Verification Script
# Run this to verify all files are in place before deployment

echo "================================================================================"
echo "  Urban Problem Reporting System - Project Verification"
echo "================================================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter for checks
PASSED=0
FAILED=0

# Function to check if file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $1 (MISSING)"
        ((FAILED++))
    fi
}

# Function to check if directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1/"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $1/ (MISSING)"
        ((FAILED++))
    fi
}

echo "📋 Documentation Files:"
check_file "README.md"
check_file "IMPLEMENTATION_CHECKLIST.md"
check_file "SESSION_SUMMARY.md"
check_file "CONTINUATION_UPDATE.md"
check_file "PROJECT_OVERVIEW.md"
check_file "MANIFEST.md"
check_file "INDEX.md"
check_file "FINAL_SUMMARY.txt"

echo ""
echo "🔧 Configuration Files:"
check_file "pom.xml"
check_file ".env.example"
check_file ".gitignore"
check_file "Dockerfile"

echo ""
echo "📁 Source Directories:"
check_dir "src/main/java/com/urbanproblems"
check_dir "src/main/java/com/urbanproblems/config"
check_dir "src/main/java/com/urbanproblems/controller"
check_dir "src/main/java/com/urbanproblems/domain"
check_dir "src/main/java/com/urbanproblems/dto"
check_dir "src/main/java/com/urbanproblems/exception"
check_dir "src/main/java/com/urbanproblems/repository"
check_dir "src/main/java/com/urbanproblems/security"
check_dir "src/main/java/com/urbanproblems/service"
check_dir "src/test/java/com/urbanproblems"

echo ""
echo "🎯 Key Source Files:"
check_file "src/main/java/com/urbanproblems/UrbanProblemReportingApplication.java"
check_file "src/main/java/com/urbanproblems/config/SecurityConfiguration.java"
check_file "src/main/java/com/urbanproblems/security/JwtTokenProvider.java"
check_file "src/main/java/com/urbanproblems/service/ComplaintService.java"
check_file "src/main/java/com/urbanproblems/service/RecurrenceService.java"
check_file "src/main/java/com/urbanproblems/service/AnalyticsService.java"
check_file "src/main/java/com/urbanproblems/controller/AuthController.java"
check_file "src/main/java/com/urbanproblems/controller/ComplaintController.java"
check_file "src/main/java/com/urbanproblems/controller/RecurrenceController.java"
check_file "src/main/java/com/urbanproblems/controller/AnalyticsController.java"

echo ""
echo "🧪 Test Files:"
check_file "src/test/java/com/urbanproblems/IntegrationTestBase.java"
check_file "src/test/java/com/urbanproblems/controller/AuthControllerIntegrationTest.java"
check_file "src/test/java/com/urbanproblems/service/ComplaintServiceTest.java"

echo ""
echo "⚙️ Resources:"
check_file "src/main/resources/application.properties"
check_file "src/test/resources/application-test.properties"

echo ""
echo "================================================================================"
echo "                            FILE COUNT SUMMARY"
echo "================================================================================"

JAVA_FILES=$(find src/main/java -name "*.java" | wc -l)
TEST_FILES=$(find src/test/java -name "*.java" | wc -l)
CONFIG_FILES=$(find . -maxdepth 1 -name "*.properties" -o -name "Dockerfile" -o -name ".env*" | wc -l)
DOCS=$(find . -maxdepth 1 -name "*.md" -o -name "*.txt" | wc -l)

echo "Java Source Files:        $JAVA_FILES"
echo "Test Files:               $TEST_FILES"
echo "Configuration Files:      $CONFIG_FILES"
echo "Documentation Files:      $DOCS"
echo ""
echo "Checks Passed: ${GREEN}$PASSED${NC}"
echo "Checks Failed: ${RED}$FAILED${NC}"

echo ""
echo "================================================================================"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ PROJECT VERIFICATION COMPLETE - ALL FILES PRESENT${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. ./mvnw clean compile    # Verify build"
    echo "  2. ./mvnw test             # Run tests"
    echo "  3. ./mvnw spring-boot:run  # Start server"
    echo ""
    exit 0
else
    echo -e "${RED}✗ VERIFICATION FAILED - MISSING $FAILED FILE(S)${NC}"
    echo ""
    echo "Please check the missing files and re-run verification."
    echo ""
    exit 1
fi
