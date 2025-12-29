# Azure Deployment Guide

This guide walks you through deploying the Kotlin Web Playground application to **Azure Container Apps** using Azure
Container Registry (ACR).

## Prerequisites

- **Azure Account**: [Create a free account](https://azure.microsoft.com/free/)
- **Azure CLI**: [Install Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli)
- **Docker**: Already installed for local development
- **Built Docker Image**: `docker build -f Dockerfile -t web-playground:latest .`

## Configuration Variables

**⚠️ IMPORTANT**: Set these variables once at the beginning. All commands in this guide use these variables.

Copy and execute this entire block in your PowerShell terminal:

```powershell
# ====================================
# Azure Configuration Variables
# ====================================
# Customize these values for your deployment

# Azure Resource Configuration
$resourceGroup = "web-playground-rg"
$location = "northeurope"  # Options: eastus, westeurope, northeurope, westus2, etc.

# Azure Container Registry (must be globally unique, lowercase, alphanumeric only, 5-50 chars)
$acrName = "vshpyntawebplaygroundacr"  # Change this to YOUR unique name

# Azure Container Apps
$envName = "web-playground-env"
$appName = "web-playground-app"

# Application Secrets (from the .env file)
$cookieEncryptionKey = "1d13f63b868ad26c46151245e1b5175c"
$cookieSigningKey = "d232897cbcc6cc89579bfbfc060632945e0dc519927c891733421f0f4a9ae48f"

# Optional: Database Configuration (for PostgreSQL setup)
$dbServerName = "web-playground-db"
$dbAdminUser = "myadmin"
$dbAdminPassword = "YourSecurePassword123!"  # Change this!

# Optional: Custom Domain and Insights
$customDomain = "www.yourdomain.com"  # Change this!
$insightsName = "web-playground-insights"

Write-Host "✅ Variables configured successfully!" -ForegroundColor Green
Write-Host "ACR Name: $acrName" -ForegroundColor Yellow
Write-Host "Resource Group: $resourceGroup" -ForegroundColor Yellow
Write-Host "Location: $location" -ForegroundColor Yellow
```

**After running this block, you can copy-paste any command from this guide without modification!**

## Table of Contents

- [Configuration Variables](#configuration-variables)
- [Step 1: Install and Login to Azure CLI](#step-1-install-and-login-to-azure-cli)
- [Step 2: Create Azure Container Registry (ACR)](#step-2-create-azure-container-registry-acr)
- [Step 3: Push Docker Image to ACR](#step-3-push-docker-image-to-acr)
- [Step 4: Deploy to Azure Container Apps](#step-4-deploy-to-azure-container-apps)
- [Step 5: Configure Environment Variables](#step-5-configure-environment-variables)
- [Step 6: Get Application URL](#step-6-get-application-url)
- [Complete Deployment Script](#complete-deployment-script-powershell)
- [Update Existing Deployment](#update-existing-deployment)
- [Azure Container Apps Features](#azure-container-apps-features)
- [Monitoring and Logs](#monitoring-and-logs)
- [Troubleshooting](#troubleshooting)
- [Cost Optimization](#cost-optimization)
- [Next Steps](#next-steps)

## Step 1: Install and Login to Azure CLI

### Install Azure CLI

**Windows (MSI Installer)**:

- Download from: [https://aka.ms/installazurecliwindows](https://aka.ms/installazurecliwindows)

**Windows (Winget)**:

```powershell
winget install -e --id Microsoft.AzureCLI
```

**macOS**:

```bash
brew install azure-cli
```

**Linux (Ubuntu/Debian)**:

```bash
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
```

### Login to Azure

**Standard login** (opens browser for authentication):

```powershell
az login
```

**If your organization requires MFA**, use device code flow:

```powershell
az login --use-device-code
```

**If you have multiple tenants**, specify tenant ID:

```powershell
az login --tenant YOUR_TENANT_ID
```

You can find your Tenant ID in Azure Portal → Microsoft Entra ID (Azure Active Directory) → Overview → Tenant ID.

### Verify Your Subscription

```powershell
# Show current account
az account show

# List all subscriptions
az account list --output table

# Set default subscription (if you have multiple)
az account set --subscription "Your Subscription Name"
```

## Step 2: Create Azure Container Registry (ACR)

> **Note**: This step uses variables from the [Configuration Variables](#configuration-variables) section. Make sure you've set them first.

### Create Resource Group

```powershell
az group create --name $resourceGroup --location $location
```

**Available locations**: `eastus`, `westeurope`, `northeurope`, `westus2`, etc. Choose the one closest to your users.

### Create Azure Container Registry

```powershell
az acr create --resource-group $resourceGroup --name $acrName --sku Basic --admin-enabled true
```

### Login to ACR

```powershell
az acr login --name $acrName
```

**If login fails**, try Docker login directly:

```powershell
# Get credentials
az acr credential show --name $acrName

# Login with Docker
docker login "$acrName.azurecr.io"
```

## Step 3: Push Docker Image to ACR

> **Note**: This step uses variables from the [Configuration Variables](#configuration-variables) section.

### Build Docker Image (if not already built)

```powershell
# Build the JAR
gradlew.bat clean build shadowJar

# Build Docker image
docker build -f Dockerfile -t web-playground:latest .
```

### Tag Image for ACR

```powershell
docker tag web-playground:latest "$acrName.azurecr.io/web-playground:latest"
```

### Push Image to ACR

```powershell
docker push "$acrName.azurecr.io/web-playground:latest"
```

The first push will take a few minutes as it uploads all layers. Subsequent pushes are incremental and much faster.

### Verify Image in ACR

```powershell
# List all repositories
az acr repository list --name $acrName --output table

# List tags for specific repository
az acr repository show-tags --name $acrName --repository web-playground --output table
```

## Step 4: Deploy to Azure Container Apps

> **Note**: This step uses variables from the [Configuration Variables](#configuration-variables) section.

### Get ACR Credentials

```powershell
$acrPassword = az acr credential show --name $acrName --query "passwords[0].value" -o tsv
```

### Create Container Apps Environment

```powershell
az containerapp env create --name $envName --resource-group $resourceGroup --location $location
```

This creates a managed environment for your container apps. Takes about 2-3 minutes.

### Deploy the Application

```powershell
az containerapp create --name $appName --resource-group $resourceGroup --environment $envName --image "$acrName.azurecr.io/web-playground:latest" --target-port 4207 --ingress external --registry-server "$acrName.azurecr.io" --registry-username $acrName --registry-password $acrPassword --cpu 0.5 --memory 1.0Gi
```

**Parameters explained**:

- `--target-port 4207`: Internal port your app listens on
- `--ingress external`: Makes the app accessible from internet
- `--cpu 0.5`: 0.5 vCPU cores
- `--memory 1.0Gi`: 1 GB memory

## Step 5: Configure Environment Variables

> **Note**: This step uses variables from the [Configuration Variables](#configuration-variables) section.

### Set Secrets (Recommended for Sensitive Data)

Store sensitive values like encryption keys as secrets:

```powershell
az containerapp secret set --name $appName --resource-group $resourceGroup --secrets cookie-encryption-key=$cookieEncryptionKey cookie-signing-key=$cookieSigningKey
```

### Update Environment Variables

```powershell
az containerapp update --name $appName --resource-group $resourceGroup --set-env-vars WEB_PLAYGROUND_ENV=prod WEB_PLAYGROUND_HTTP_PORT=4207 WEB_PLAYGROUND_DB_USER= WEB_PLAYGROUND_DB_PASSWORD= WEB_PLAYGROUND_DB_URL="jdbc:h2:./build/prod;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;" WEB_PLAYGROUND_USE_SECURE_COOKIE=true WEB_PLAYGROUND_COOKIE_ENCRYPTION_KEY=secretref:cookie-encryption-key WEB_PLAYGROUND_COOKIE_SIGNING_KEY=secretref:cookie-signing-key
```

**Note**: `secretref:cookie-encryption-key` references the secret created in the previous step.

### Verify Environment Variables

```powershell
az containerapp show --name $appName --resource-group $resourceGroup --query properties.template.containers[0].env
```

## Step 6: Get Application URL

> **Note**: This step uses variables from the [Configuration Variables](#configuration-variables) section.

### Retrieve Your Application URL

```powershell
$appUrl = az containerapp show --name $appName --resource-group $resourceGroup --query properties.configuration.ingress.fqdn -o tsv
Write-Host "Application URL: https://$appUrl" -ForegroundColor Green
```

Your application will be accessible at: `https://web-playground-app.<region>.azurecontainerapps.io`

### Test Your Deployment

```powershell
# Test endpoints
curl "https://$appUrl/api"
curl "https://$appUrl/api/json_test"
curl "https://$appUrl/api/users/1"
```

Or open in browser:
```powershell
Start-Process "https://$appUrl"
```

Or open in browser:

- `https://<your-app-url>`
- `https://<your-app-url>/api`

## Complete Deployment Script (PowerShell)

Here's the complete script with all commands. Replace `YOUR_UNIQUE_ACR_NAME` with your chosen name:

```powershell
# ====================================
# Azure Container Apps Deployment Script
# ====================================

# Variables (customize these)
$acrName = "YOUR_UNIQUE_ACR_NAME"  # e.g., "vshpyntawebplaygroundacr"
$resourceGroup = "web-playground-rg"
$location = "northeurope"
$appName = "web-playground-app"
$envName = "web-playground-env"

# Secrets (replace with your actual values from .env file)
$cookieEncryptionKey = "1d13f63b868ad26c46151245e1b5175c"
$cookieSigningKey = "d232897cbcc6cc89579bfbfc060632945e0dc519927c891733421f0f4a9ae48f"

# ====================================
# Deployment Steps
# ====================================

# 1. Login to Azure
Write-Host "Step 1: Logging in to Azure..." -ForegroundColor Green
az login

# 2. Create resource group
Write-Host "Step 2: Creating resource group..." -ForegroundColor Green
az group create --name $resourceGroup --location $location

# 3. Create Azure Container Registry
Write-Host "Step 3: Creating Azure Container Registry..." -ForegroundColor Green
az acr create --resource-group $resourceGroup --name $acrName --sku Basic --admin-enabled true

# 4. Login to ACR
Write-Host "Step 4: Logging in to ACR..." -ForegroundColor Green
az acr login --name $acrName

# 5. Tag and push Docker image
Write-Host "Step 5: Tagging and pushing Docker image..." -ForegroundColor Green
docker tag web-playground:latest "$acrName.azurecr.io/web-playground:latest"
docker push "$acrName.azurecr.io/web-playground:latest"

# 6. Verify image
Write-Host "Step 6: Verifying image in ACR..." -ForegroundColor Green
az acr repository list --name $acrName --output table

# 7. Get ACR credentials
Write-Host "Step 7: Getting ACR credentials..." -ForegroundColor Green
$acrPassword = az acr credential show --name $acrName --query "passwords[0].value" -o tsv

# 8. Create Container Apps environment
Write-Host "Step 8: Creating Container Apps environment..." -ForegroundColor Green
az containerapp env create --name $envName --resource-group $resourceGroup --location $location

# 9. Deploy application
Write-Host "Step 9: Deploying application to Azure Container Apps..." -ForegroundColor Green
az containerapp create --name $appName --resource-group $resourceGroup --environment $envName --image "$acrName.azurecr.io/web-playground:latest" --target-port 4207 --ingress external --registry-server "$acrName.azurecr.io" --registry-username $acrName --registry-password $acrPassword --cpu 0.5 --memory 1.0Gi

# 10. Set secrets
Write-Host "Step 10: Setting secrets..." -ForegroundColor Green
az containerapp secret set --name $appName --resource-group $resourceGroup --secrets cookie-encryption-key=$cookieEncryptionKey cookie-signing-key=$cookieSigningKey

# 11. Configure environment variables
Write-Host "Step 11: Configuring environment variables..." -ForegroundColor Green
az containerapp update --name $appName --resource-group $resourceGroup --set-env-vars WEB_PLAYGROUND_ENV=prod WEB_PLAYGROUND_HTTP_PORT=4207 WEB_PLAYGROUND_DB_USER= WEB_PLAYGROUND_DB_PASSWORD= WEB_PLAYGROUND_DB_URL="jdbc:h2:./build/prod;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;" WEB_PLAYGROUND_USE_SECURE_COOKIE=true WEB_PLAYGROUND_COOKIE_ENCRYPTION_KEY=secretref:cookie-encryption-key WEB_PLAYGROUND_COOKIE_SIGNING_KEY=secretref:cookie-signing-key

# 12. Get application URL
Write-Host "Step 12: Deployment complete!" -ForegroundColor Green
Write-Host "Application URL:" -ForegroundColor Yellow
az containerapp show --name $appName --resource-group $resourceGroup --query properties.configuration.ingress.fqdn -o tsv
```

## Update Existing Deployment

When you make changes to your application and want to deploy a new version:

> **Note**: This section uses variables from the [Configuration Variables](#configuration-variables) section.

### Quick Update Process

```powershell
# 1. Rebuild JAR (if code changed)
gradlew.bat clean build shadowJar

# 2. Rebuild Docker image
docker build -f Dockerfile -t web-playground:latest .

# 3. Tag with ACR registry
docker tag web-playground:latest "$acrName.azurecr.io/web-playground:latest"

# 4. Push to ACR
docker push "$acrName.azurecr.io/web-playground:latest"

# 5. Update Container App (triggers new revision with zero-downtime deployment)
az containerapp update --name $appName --resource-group $resourceGroup --image "$acrName.azurecr.io/web-playground:latest"
```

### Update Only Configuration

If you only need to change environment variables:

```powershell
az containerapp update --name $appName --resource-group $resourceGroup --set-env-vars KEY=VALUE
```

## Azure Container Apps Features

Your deployed application automatically benefits from:

### Automatic HTTPS

- ✅ Free SSL/TLS certificates managed by Azure
- ✅ Automatic certificate renewal
- ✅ HTTPS-only traffic (HTTP redirects to HTTPS)

### Auto-scaling

- ✅ Scales from 0 to 10 replicas based on HTTP traffic
- ✅ Scale to zero when idle (no compute cost)
- ✅ Automatic scale-out under load
- ✅ Configurable scaling rules

### Zero-downtime Deployments

- ✅ Rolling updates with new revisions
- ✅ Traffic splitting between revisions
- ✅ Instant rollback capability
- ✅ Blue-green deployment support

### Health Monitoring

- ✅ Automatic health checks
- ✅ Automatic container restarts on failure
- ✅ Built-in application insights integration
- ✅ Container metrics and logs

### Cost-effective

- ✅ Pay only for actual usage
- ✅ Free tier: 180,000 vCPU-seconds and 360,000 GiB-seconds per month
- ✅ Can scale to zero (no cost when idle)

## Monitoring and Logs

> **Note**: This section uses variables from the [Configuration Variables](#configuration-variables) section.

### View Live Logs

```powershell
# Stream live logs
az containerapp logs show --name $appName --resource-group $resourceGroup --follow

# View recent logs (last 50 lines)
az containerapp logs show --name $appName --resource-group $resourceGroup --tail 50
```

### View Container App Details

```powershell
# Show all details
az containerapp show --name $appName --resource-group $resourceGroup

# Show only URL
az containerapp show --name $appName --resource-group $resourceGroup --query properties.configuration.ingress.fqdn -o tsv

# Show environment variables
az containerapp show --name $appName --resource-group $resourceGroup --query properties.template.containers[0].env
```

### Manage Revisions

Azure Container Apps uses **revisions** for deployment history:

```powershell
# List all revisions
az containerapp revision list --name $appName --resource-group $resourceGroup --output table

# Show specific revision
az containerapp revision show --name $appName --resource-group $resourceGroup --revision REVISION_NAME

# Activate a specific revision (rollback)
az containerapp revision activate --name $appName --resource-group $resourceGroup --revision REVISION_NAME

# Deactivate a revision
az containerapp revision deactivate --name $appName --resource-group $resourceGroup --revision REVISION_NAME
```

### Scale Configuration

```powershell
# Set min/max replicas
az containerapp update --name $appName --resource-group $resourceGroup --min-replicas 0 --max-replicas 5

# View current scale settings
az containerapp show --name $appName --resource-group $resourceGroup --query properties.template.scale
```

## Troubleshooting

### Common Issues and Solutions

| Issue                                 | Solution                                                                                                                                  |
|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **ACR login fails**                   | Run `az acr login --name YOUR_ACR_NAME` or use `docker login YOUR_ACR_NAME.azurecr.io` with credentials                                   |
| **Image push is slow**                | First push uploads all layers (~250MB); subsequent pushes are incremental                                                                 |
| **Container app won't start**         | Check logs: `az containerapp logs show --name web-playground-app --resource-group web-playground-rg --tail 100`                           |
| **Environment variables not applied** | Verify: `az containerapp show --name web-playground-app --resource-group web-playground-rg --query properties.template.containers[0].env` |
| **ACR name already taken**            | ACR names must be globally unique; try a different name (e.g., add your initials)                                                         |
| **MFA required for login**            | Use `az login --use-device-code`                                                                                                          |
| **Application shows 502 error**       | Check target port matches your app: `--target-port 4207`                                                                                  |
| **Database connection fails**         | Verify `WEB_PLAYGROUND_DB_URL` environment variable is set correctly                                                                      |
| **Secrets not working**               | Ensure you use `secretref:secret-name` format in environment variables                                                                    |

### Debug Commands

> **Note**: These commands use variables from the [Configuration Variables](#configuration-variables) section.

```powershell

# Check Container App status
az containerapp show --name $appName --resource-group $resourceGroup --query properties.runningStatus

# View all environment variables
az containerapp show --name $appName --resource-group $resourceGroup --query properties.template.containers[0].env

# View secrets (names only, not values)
az containerapp secret show --name $appName --resource-group $resourceGroup

# Check ingress configuration
az containerapp show --name $appName --resource-group $resourceGroup --query properties.configuration.ingress
```

## Clean Up Resources

When you want to delete everything and stop incurring charges:

> **Note**: This section uses variables from the [Configuration Variables](#configuration-variables) section.

### Delete Entire Resource Group

```powershell
# Delete resource group (removes all resources: ACR, Container App, environment)
az group delete --name $resourceGroup --yes --no-wait
```

### Delete Individual Resources

```powershell
# Delete Container App only
az containerapp delete --name $appName --resource-group $resourceGroup --yes

# Delete Container App environment
az containerapp env delete --name $envName --resource-group $resourceGroup --yes

# Delete ACR
az acr delete --name $acrName --resource-group $resourceGroup --yes
```

## Cost Optimization

### Azure Container Apps Pricing

**Free tier** (per subscription):

- 180,000 vCPU-seconds per month
- 360,000 GiB-seconds per month
- 2 million requests per month

**After free tier**:

- vCPU: ~$0.000024 per vCPU-second
- Memory: ~$0.0000025 per GiB-second
- Requests: $0.40 per million requests

### Azure Container Registry Pricing

- **Basic**: ~$5/month for 10 GB storage
- **Standard**: ~$20/month for 100 GB storage
- **Premium**: ~$50/month for 500 GB storage

### Cost-saving Tips

1. **Scale to zero**: Configure `--min-replicas 0` for non-production apps
2. **Right-size resources**: Use `--cpu 0.25 --memory 0.5Gi` for small workloads
3. **Use Basic ACR tier**: Sufficient for most small projects
4. **Delete unused images**: Clean up old images in ACR regularly
5. **Use single environment**: Share Container Apps environment across multiple apps

### Estimate Your Costs

For a small application running 24/7 with 0.5 vCPU and 1 GB memory:

- **Monthly compute**: ~$12-15
- **ACR Basic**: ~$5
- **Total**: ~$17-20/month

If app scales to zero during idle hours (e.g., 50% idle time), cost reduces by ~50%.

## Next Steps

### Add Azure Database for PostgreSQL

Replace H2 embedded database with managed PostgreSQL:

> **Note**: This section uses variables from the [Configuration Variables](#configuration-variables) section.

```powershell

# Create PostgreSQL server
az postgres flexible-server create --resource-group $resourceGroup --name $dbServerName --location $location --admin-user $dbAdminUser --admin-password $dbAdminPassword --sku-name Standard_B1ms --storage-size 32

# Get connection string
az postgres flexible-server show-connection-string --server-name $dbServerName
```

Update environment variables:

```powershell
$dbUrl = "jdbc:postgresql://$dbServerName.postgres.database.azure.com:5432/postgres?ssl=true"
az containerapp update --name $appName --resource-group $resourceGroup --set-env-vars WEB_PLAYGROUND_DB_URL="$dbUrl" WEB_PLAYGROUND_DB_USER=$dbAdminUser WEB_PLAYGROUND_DB_PASSWORD=$dbAdminPassword
```

### Set up CI/CD with GitHub Actions

Create `.github/workflows/azure-deploy.yml`:

```yaml
name: Deploy to Azure Container Apps

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 24
        uses: actions/setup-java@v3
        with:
          java-version: '24'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew shadowJar

      - name: Login to Azure
        uses: azure/login@v1
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Build and push to ACR
        run: |
          az acr build --registry ${{ secrets.ACR_NAME }} --image web-playground:${{ github.sha }} .

      - name: Deploy to Container Apps
        run: |
          az containerapp update \
            --name web-playground-app \
            --resource-group web-playground-rg \
            --image ${{ secrets.ACR_NAME }}.azurecr.io/web-playground:${{ github.sha }}
```

### Configure Custom Domain

> **Note**: This section uses variables from the [Configuration Variables](#configuration-variables) section.

```powershell
# Add custom domain
az containerapp hostname add --name $appName --resource-group $resourceGroup --hostname $customDomain

# Bind certificate
az containerapp hostname bind --name $appName --resource-group $resourceGroup --hostname $customDomain --environment $envName --validation-method CNAME
```

### Enable Application Insights

> **Note**: This section uses variables from the [Configuration Variables](#configuration-variables) section.

```powershell

# Create Application Insights
az monitor app-insights component create --app $insightsName --location $location --resource-group $resourceGroup

# Get instrumentation key
$instrumentationKey = az monitor app-insights component show --app $insightsName --resource-group $resourceGroup --query instrumentationKey -o tsv

# Update Container App
az containerapp update --name $appName --resource-group $resourceGroup --set-env-vars APPLICATIONINSIGHTS_CONNECTION_STRING="InstrumentationKey=$instrumentationKey"
```

## Additional Resources

- [Azure Container Apps Documentation](https://docs.microsoft.com/azure/container-apps/)
- [Azure Container Registry Documentation](https://docs.microsoft.com/azure/container-registry/)
- [Azure CLI Reference](https://docs.microsoft.com/cli/azure/)
- [Azure Pricing Calculator](https://azure.microsoft.com/pricing/calculator/)
- [GitHub Actions for Azure](https://github.com/Azure/actions)

---

**Need help?** Open an issue in the GitHub repository or consult
the [Azure documentation](https://docs.microsoft.com/azure/).

