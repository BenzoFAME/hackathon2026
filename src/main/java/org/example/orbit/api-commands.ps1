# =============================================
# ORBIT API - PowerShell команды для тестирования
# =============================================

# --- ADMIN ---

# Загрузить спутники в БД (запускать первым, ждать ~2 мин)
Invoke-WebRequest -UseBasicParsing -Method POST -Uri "http://localhost:8888/api/admin/reload" -TimeoutSec 300 | Select-Object -ExpandProperty Content

# --- ПОИСК И ФИЛЬТРЫ ---

# Список спутников (первые 10)
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites?page=0&size=10" | Select-Object -ExpandProperty Content

# Поиск по имени
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/search?query=ISS" | Select-Object -ExpandProperty Content

# Поиск по NORAD ID
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/search?query=25544" | Select-Object -ExpandProperty Content

# Фильтр по типу орбиты
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/filter?orbitType=LEO_LOW_EARTH_ORBIT" | Select-Object -ExpandProperty Content

# Фильтр по стране
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/filter?country=USA_UNITED_STATES" | Select-Object -ExpandProperty Content

# Фильтр по типу объекта
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/filter?objectType=PAYLOAD_PAYLOAD" | Select-Object -ExpandProperty Content

# Фильтр по всем параметрам сразу
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/filter?country=USA_UNITED_STATES&orbitType=LEO_LOW_EARTH_ORBIT&objectType=PAYLOAD_PAYLOAD" | Select-Object -ExpandProperty Content

# --- ПОЗИЦИЯ (SGP4) ---

# Текущая позиция МКС
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/25544/position" | Select-Object -ExpandProperty Content

# Позиция МКС в конкретный момент времени
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/25544/position?time=2026-03-21T10:00:00Z" | Select-Object -ExpandProperty Content

# --- КАРТОЧКА ---

# Карточка МКС (все параметры + позиция)
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/25544/card" | Select-Object -ExpandProperty Content

# --- ТРЕК ОРБИТЫ ---

# Трек орбиты МКС (один полный оборот ~90 мин)
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/25544/track" | Select-Object -ExpandProperty Content

# --- PASS PREDICTION ---

# Пролёты МКС над Москвой на 24 часа
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/25544/passes?lat=55.75&lon=37.61&hours=24" | Select-Object -ExpandProperty Content

# Пролёты МКС над Амстердамом на 12 часов
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/25544/passes?lat=52.37&lon=4.89&hours=12" | Select-Object -ExpandProperty Content

# Пролёты МКС над Пекином на 24 часа
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8888/api/satellites/25544/passes?lat=39.90&lon=116.39&hours=24" | Select-Object -ExpandProperty Content

# =============================================
# NORAD ID популярных спутников:
# 25544 - ISS (МКС)
# 20580 - Hubble Space Telescope
# 39084 - GPS BIIA-23
# 43013 - NOAA 20
# =============================================