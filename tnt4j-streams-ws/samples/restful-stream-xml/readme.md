## Weather forecast streaming sample

This sample gets the weather for 3 cities, using different time intervals and repeat counts.

To use this weather forecast sample, you will have to sign up for a free subscription to one of the weather services at
[OpenWeatherMap](https://openweathermap.org/api) (for example, Current Weather Data or 5 Day Forecast) and get an API key (APPID), which
must be substituted (replace placeholder `<YOUR_APP_TOKEN>`) in the URL line of streams config
file [tnt-data-source.xml](./tnt-data-source.xml). **NOTE:** it may take up to **1 hour after registering**, to receive your APPID.

You can also change the three city names in the config file.

The API documentation in [OpenWeatherMap](https://openweathermap.org/current) describes the format of the URL lines.
