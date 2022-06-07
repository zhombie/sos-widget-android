echo "=== [BEGIN] SOS Widget DEV upload ==="
./gradlew assembleDev appDistributionUploadDevDebug
echo "=== [END] SOS Widget DEV upload ==="
echo ""
echo "=== [BEGIN] SOS Widget upload ==="
./gradlew assembleProd appDistributionUploadProdDebug
echo "=== [END] SOS Widget upload ==="
