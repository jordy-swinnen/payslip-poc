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
- **AI-Powered Extraction**: Uses OpenAI's GPT-4o vision model to intelligently extract payslip data
- **Structured Output**: Returns data in a well-defined JSON format
- **Vector Store Integration**: Automatically indexes extracted payslip data into Elasticsearch
- **Similarity Search**: Find similar payslips by employee name or national ID using vector embeddings
- **Question Answering**: Ask natural language questions about uploaded payslips
- **Document Section Retrieval**: Retrieve specific payslip sections by document ID
- **RESTful API**: Simple HTTP endpoints for file upload, data extraction, Q&A, and similarity search
- **Docker Compose Support**: Easy setup with containerized Elasticsearch

## API Endpoints

A ready-to-use Postman collection is available in the root of the project: `PostmanCollection.json`

### 1. Extract Payslip Data

**POST** `/api/payslip/extract`

- **Content-Type**: `multipart/form-data`
- **Request**: Upload a payslip file
- **Response**: Structured JSON with extracted payslip data
- Automatically indexes the data into the vector store

### 2. Ask Questions About Payslip

**POST** `/api/payslip/ask`

- **Content-Type**: `multipart/form-data`
- **Request**: Upload a payslip file and provide a question
- **Response**: AI-generated answer based on the payslip content

### 3. Get Payslip Section

**GET** `/api/payslip/section/{docId}`

- **Path Parameter**: `docId` - Document identifier
- **Response**: Specific payslip section data (200) or 404 if not found

### 4. Find Similar Payslips

**GET** `/api/payslip/similar`

- **Query Parameters**:
    - `nationalId` (optional) - Employee national ID
    - `employeeName` (optional) - Employee name
    - `limit` (optional, default: 3) - Max number of results
- **Response**: List of similar payslips ranked by relevance

## Extracted Data Structure

The API extracts the following information from payslips:

### Personal Information
- Employee name
- Address
- National ID
- Marital status
- Number of dependents

### Employer Information

- Employer name
- Address
- Employer number

### Employment Details

- Employee number
- Job title
- Employment status
- Pay category
- Base monthly salary

### Pay Period

- Period start and end dates
- Payment date
- Currency

### Financial Information

- Gross pay
- Taxable amount
- Social security contributions
- Withholding tax
- Net pay
- Payment IBAN and BIC

### Extras

- Meal voucher contributions (employer and employee)
- Meal voucher count
- Benefits in kind (company car, phone, internet, etc.)
- Allowances and reimbursements

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring AI 1.0.3** (OpenAI integration)
- **Spring AI Elasticsearch Vector Store** (for vector embeddings and similarity search)
- **Spring AI RAG** (Retrieval-Augmented Generation for Q&A)
- **Elasticsearch 8.13.4** (vector database)
- **Apache PDFBox 3.0.5** (PDF processing)
- **Lombok** (boilerplate reduction)
- **Gradle** (build tool)
- **Docker Compose** (for Elasticsearch deployment)

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for Elasticsearch)
- OpenAI API key

## Setup and Configuration

### 1. Start Elasticsearch

The application includes a `docker-compose.yml` file that sets up Elasticsearch:
