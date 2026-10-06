ALTER TABLE IF EXISTS raster_location_zonal_stats
    ADD COLUMN IF NOT EXISTS tag VARCHAR(255);

ALTER TABLE IF EXISTS raster_location_zonal_stats_aud
    ADD COLUMN IF NOT EXISTS tag VARCHAR(255);
