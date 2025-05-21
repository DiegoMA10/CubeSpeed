@echo off
echo Getting SHA-1 fingerprint for the app...
cd %~dp0
gradlew signingReport