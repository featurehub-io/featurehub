import 'package:mrapi/api.dart';
import 'package:test/test.dart';

void main() {
  test('person equals works expected', ()
  {
    Person a = Person(id: PersonId(id: ''), personType: PersonType.person, name: 'Rob', email: 'rob@mailinator.com', version: -1);

    Person b = Person(id: PersonId(id: ''), personType: PersonType.person, name: 'Rob', email: 'rob@mailinator.com', version: -1);

//    Set<int> vals = Set<int>();
//    vals.add(1);
//    expect(vals.contains(1), true);
    Set<Person> people = Set<Person>();
    people.add(a);
    print(people);
    expect(a == b, true);
    expect(people.contains(b), true);
//    expect(a == a, true);
  });
}
