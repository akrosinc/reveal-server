CREATE MATERIALIZED VIEW IF NOT EXISTS location_metadata_double_aggregate
AS
SELECT uuid_generate_v4()   as identifier
     , lp.identifier        as locationParentIdentifier
     , lp.name              as parentName
     , l.identifier         as locationIdentifier
     , l.name               as locationName
     , meta.tag             as tag
     , meta.tag_key         as tagKey
     , meta.value           as value
     , meta.plan_identifier as planIdentifier
from (
         SELECT obj ->> 'tag'                                                as tag,
                obj ->> 'tagKey'                                             as tag_key,
                cast(obj -> 'current' -> 'value' ->> 'valueDouble' as float) as value,
                lm.location_identifier                                       as location_identifier,
                obj -> 'current' -> 'meta' ->> 'planId'                      as plan_identifier
         From location_metadata lm,
              jsonb_array_elements(lm.entity_value -> 'metadataObjs')
                  with ordinality arr(obj, pos)
         WHERE (obj -> 'current' -> 'value' ->> 'valueDouble') is not null
     ) as meta
         left join location l
                   on l.identifier = meta.location_identifier
         left join location_relationships lr on l.identifier = lr.location_identifier
         left join location lp on lp.identifier = lr.location_parent_identifier;

CREATE UNIQUE INDEX ON location_metadata_double_aggregate (identifier);
