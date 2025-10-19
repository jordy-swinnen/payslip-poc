# Payslip Data Extraction API

A proof-of-concept Spring Boot application that uses Spring AI with OpenAI to extract structured data from payslip
documents (PDF or image files) and enables similarity search using vector embeddings.

## Overview

This application demonstrates the use of Spring AI to process payslip documents and extract key information such as
employee details, payment dates, earnings, and deductions. It leverages OpenAI's vision capabilities to analyze document
images, returns structured JSON data, and stores the extracted information in an Elasticsearch vector store for
similarity-based retrieval.

## Features

- **Multiple Format Support**: Accepts both PDF and image files (PNG, JPEG, etc.)
- **AI-Powered Extraction**: Uses OpenAI's vision model to intelligently extract payslip data
- **Structured Output**: Returns data in a well-defined JSON format
- **Vector Store Integration**: Automatically indexes extracted payslip data into Elasticsearch
- **Similarity Search**: Find similar payslips by employee name or national ID using vector embeddings
- **RESTful API**: Simple HTTP endpoints for file upload, data extraction, and similarity search
- **Docker Compose Support**: Easy setup with containerized Elasticsearch

## Extracted Data

The API extracts the following information from payslips:

### Personal Information
- Employee name
- National ID

### Pay Period

- Month key (YYYY-MM format)
- Payment date

### Earnings

- Description and amount for each earning line item (e.g., Basic Salary, Allowances)

### Deductions

- Description and amount for each deduction line item (e.g., Tax, Insurance)

### Totals
- Gross pay amount
- Total deductions
- Net pay amount

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring AI 1.0.3** (OpenAI integration)
- **Spring AI Elasticsearch Vector Store** (for vector embeddings and similarity search)
- **Elasticsearch 8.13.4** (vector database)
- **Apache PDFBox 3.0.5** (PDF processing)
- **Lombok** (boilerplate reduction)
- **Gradle** (build tool)
- **Docker Compose** (for Elasticsearch deployment)

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for Elasticsearch)
- OpenAI API key

## Running the Application

### 1. Start Elasticsearch

The application includes a `docker-compose.yml` file that sets up Elasticsearch:
