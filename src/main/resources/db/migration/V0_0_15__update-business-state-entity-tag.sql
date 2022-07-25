UPDATE entity_tag
SET generated= false,
    generation_formula=null,
    referenced_fields=null,
    aggregation_method=null,
    scope='Plan',
    result_expression=null,
    is_result_literal= true,
    add_to_metadata= true
WHERE tag = 'business-status';

UPDATE entity_tag
SET add_to_metadata=false
WHERE tag in
      ('supervisor-distributed-PZQ', 'supervisor-distributed-MEB', 'supervisor-distributed-ALB',
       'supervisor-returned-ALB', 'supervisor-returned-PZQ', 'supervisor-returned-MEB',
       'cdd-remaining-MEB', 'cdd-remaining-PZQ', 'cdd-remaining-ALB')