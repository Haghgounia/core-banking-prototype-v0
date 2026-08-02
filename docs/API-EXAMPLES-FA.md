# نمونه API

## Catalog

```http
GET /api/v1/catalog
```

## Descriptor استان

```http
GET /api/v1/catalog/provinces
```

## جست‌وجوی شهرستان

```http
GET /api/v1/reference/counties?text=کرج&page=0&size=20&sortBy=countyName&direction=asc
```

## ایجاد استان

```http
POST /api/v1/reference/provinces
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "provinceCode": "99",
  "provinceName": "استان نمونه",
  "provinceEnglishName": "Sample Province",
  "diallingCode": "099",
  "countryId": 71,
  "censusHousehold": null,
  "censusPopulation": null,
  "censusMale": null,
  "censusFemale": null,
  "geoType": 1,
  "isActive": true,
  "sortOrder": 99
}
```

## ایجاد شهرستان

```http
POST /api/v1/reference/counties
Content-Type: application/json
X-User-Id: 1
```

```json
{
  "countyCode": "9999",
  "countyName": "شهرستان نمونه",
  "countyEnglishName": "Sample County",
  "provinceId": 1,
  "censusHousehold": null,
  "censusPopulation": null,
  "censusMale": null,
  "censusFemale": null,
  "geoType": 2,
  "isActive": true,
  "sortOrder": 1
}
```

## Lookup وابسته

```http
GET /api/v1/reference/counties/lookup?parentId=1
GET /api/v1/reference/districts/lookup?parentId=10
GET /api/v1/reference/rural-districts/lookup?parentId=100
```

## درخت جغرافیایی

```http
GET /api/v1/geography/tree/roots
GET /api/v1/geography/tree/provinces/1/children
GET /api/v1/geography/tree/districts/100/children
```
