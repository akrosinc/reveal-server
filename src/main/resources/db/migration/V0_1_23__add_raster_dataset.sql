CREATE TABLE IF NOT EXISTS raster_dataset (
  identifier            uuid PRIMARY KEY,
  map_layer_identifier  uuid REFERENCES map_layer (id),
    dataset_identifier    varchar(255),
    name                  varchar(255),
    color_ramp            varchar(255),
    simulation_identifier uuid NOT NULL REFERENCES simulation (identifier)
    );


CREATE INDEX IF NOT EXISTS idx_raster_dataset_simulation_identifier
    ON raster_dataset (simulation_identifier);