# Payslip Data Extraction API

A proof-of-concept Spring Boot application that uses Spring AI with OpenAI to extract structured data from payslip documents (PDF or image files).

## Overview

This application demonstrates the use of Spring AI to process payslip documents and extract key information such as employee details, payment dates, and salary amounts. It leverages OpenAI's vision capabilities to analyze document images and return structured JSON data.

## Features

- **Multiple Format Support**: Accepts both PDF and image files (PNG, JPEG, etc.)
- **AI-Powered Extraction**: Uses OpenAI's vision model to intelligently extract payslip data
- **Structured Output**: Returns data in a well-defined JSON format
- **RESTful API**: Simple HTTP endpoint for file upload and data extraction

## Extracted Data

The API extracts the following information from payslips:
- Employee name
- Employer name
- Pay period start and end dates
- Payment date
- Currency (ISO code)
- Gross pay amount
- Net pay amount

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring AI 1.0.3** (OpenAI integration)
- **Apache PDFBox 3.0.5** (PDF processing)
- **Lombok** (boilerplate reduction)
- **Gradle** (build tool)

## Prerequisites

- Java 21 or higher
- OpenAI API key

## Configuration

Set your OpenAI API key in `application.properties` or `application.yml`:
