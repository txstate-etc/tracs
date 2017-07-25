get_confidential_from_person {
  select confidential from person where netid=?
}

get_confidential_map_from_person {
  select netid, confidential from person where netid in ?
}