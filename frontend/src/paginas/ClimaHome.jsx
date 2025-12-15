
import { useEffect, useState } from "react";
mport { climaApi } from "../api/clima";

const API_KEY = import.meta.env.VITE_WEATHER_API_KEY;

export default function ClimaHome() {
  const [cities, setCities] = useState([]);          
  const [selectedQuery, setSelectedQuery] = useState(""); 
  const [weather, setWeather] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  
  
  
  useEffect(() => {
    if (!API_KEY) {
      setError("No se configuró la llave de clima (VITE_WEATHER_API_KEY).");
      return;
    }

    (async () => {
      try {
        setLoading(true);
        setError("");
        setWeather(null);

        
        const { data } = await climaApi.listCities();
        const list = Array.isArray(data) ? data : [];

        setCities(list);

        
        if (list.length > 0) {
          const firstQuery = list[0].weatherQuery;
          setSelectedQuery(firstQuery);
          await cargarClima(firstQuery);
        } else {
          setError("No hay ciudades configuradas para el clima.");
        }
      } catch (e) {
        console.error(e);
        setError("No se pudieron cargar las ciudades de clima.");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  
  
  
  const cargarClima = async (query) => {
    if (!API_KEY || !query) return;
    try {
      setLoading(true);
      setError("");
      setWeather(null);

      const url = `https://api.openweathermap.org/data/2.5/weather?q=${encodeURIComponent(
        query
      )}&units=metric&lang=es&appid=${API_KEY}`;

      const resp = await fetch(url);
      if (!resp.ok) {
        throw new Error("No se pudo obtener el clima");
      }
      const data = await resp.json();

      const w = {
        ciudad: data.name,
        pais: data.sys?.country,
        temp: data.main?.temp,
        tempMin: data.main?.temp_min,
        tempMax: data.main?.temp_max,
        humedad: data.main?.humidity,
        descripcion: data.weather?.[0]?.description,
        icon: data.weather?.[0]?.icon,
      };
      setWeather(w);
    } catch (e) {
      console.error(e);
      setError("No se pudo cargar el clima para esta ciudad.");
    } finally {
      setLoading(false);
    }
  };

  
  
  
  const onChangeCity = (e) => {
    const query = e.target.value;        
    setSelectedQuery(query);
    cargarClima(query);
  };

  return (
    <section className="home-block home-weather">
      <div className="home-weather__header">
        <div>
          <h2 className="home-block__title">Clima en destinos clave</h2>
          <p className="home-weather__subtitle">
            Consulta el clima actual en algunos de los destinos más frecuentes
            de GuateFly.
          </p>
        </div>
        <div>
          <label className="label" style={{ fontSize: 13 }}>
            Selecciona destino
          </label>
          <select
            className="input"
            value={selectedQuery}
            onChange={onChangeCity}
            disabled={cities.length === 0}
          >
            {cities.map((c) => (
              <option key={c.idCiudad} value={c.weatherQuery}>
                {c.ciudad}
                {c.pais ? `, ${c.pais}` : ""}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="error" style={{ marginTop: 8 }} role="alert">
          {error}
        </div>
      )}

      <div className="home-weather__content">
        {loading && <p>Cargando clima…</p>}

        {!loading && weather && (
          <div className="home-weather__card">
            <div className="home-weather__main">
              <div>
                <div className="home-weather__city">
                  {weather.ciudad}
                  {weather.pais ? `, ${weather.pais}` : ""}
                </div>
                <div className="home-weather__temp">
                  {Math.round(weather.temp)}°C
                </div>
                <div className="home-weather__desc">
                  {weather.descripcion}
                </div>
              </div>
              {weather.icon && (
                <img
                  className="home-weather__icon"
                  src={`https://openweathermap.org/img/wn/${weather.icon}@2x.png`}
                  alt={weather.descripcion || "Clima"}
                />
              )}
            </div>

            <div className="home-weather__extra">
              <div>
                <span>Mínima</span>
                <strong>{Math.round(weather.tempMin)}°C</strong>
              </div>
              <div>
                <span>Máxima</span>
                <strong>{Math.round(weather.tempMax)}°C</strong>
              </div>
              <div>
                <span>Humedad</span>
                <strong>{weather.humedad}%</strong>
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
