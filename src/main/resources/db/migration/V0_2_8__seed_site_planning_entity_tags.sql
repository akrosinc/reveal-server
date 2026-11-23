-- Seeds the site planning entity tags (one tag per count, no aggregate tags).
-- Idempotent: a tag whose name already exists is skipped (entity_tag.tag has no unique constraint).

INSERT INTO entity_tag (identifier, tag, value_type, definition, description, aggregation_method,
                        is_aggregate, is_public, is_deleting, simulation_display)
SELECT uuid_generate_v4(),
       s.tag,
       'integer',
       s.description,
       s.description,
       NULL,
       false,
       true,
       false,
       false
FROM (VALUES ('moh_population', 'MOH population'),
             ('user_population', 'User population'),
             ('zones_count', 'Zones'),
             ('schools_count', 'Schools'),
             ('sites_outreach_added', 'Outreach added'),
             ('sites_outreach_to_add', 'Outreach to add'),
             ('sites_static_added', 'Static added'),
             ('sites_static_to_add', 'Static to add'),
             ('sites_h2r_added', 'H2R added'),
             ('sites_h2r_to_add', 'H2R to add'),
             ('sites_mobile_added', 'Mobile added'),
             ('sites_mobile_to_add', 'Mobile to add'),
             ('sites_school_added', 'School added'),
             ('sites_school_to_add', 'School to add'),
             ('sites_hpv_added', 'HPV added'),
             ('sites_hpv_to_add', 'HPV to add'),
             ('total_sites_to_add', 'Total sites to add'))
         AS s (tag, description)
WHERE NOT EXISTS (SELECT 1 FROM entity_tag et WHERE et.tag = s.tag);