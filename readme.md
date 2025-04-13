# State Machine With PostgreSQL

## Background
Statemachine is a good programming model for orchestration use cases like payments. There are some statemachine products in the industry like Spring Statemachine, Cola Statemachine etc. While Spring Statemachine is heavy and stateful, Cola cannot support multiple targets.
We want to build a lightweight, stateless, EDA oriented statemachine for our use cases.

## Features
- With kafka as the event orchestrator. This branch is use DB to simulate PubSub.
- Database as the state store, with atomic SQL operation for concurrent state merging.
- State Formula for state flow forks and joins
  > With Antlr4 defined DSL, we are able to derive7 a final state by different state combinations.
  >  ```
  >  s1 & s2 & s3 => s4;
  >  s1 | s2 & s3 => s4;
  >  (s1 & s2) | (s3 & s4) => s5;
  >  ```

## Caution
The table `sm_pub_sub` need to be regularly cleaned for those `processed='Y'` cases, then vacuum (will lock table, pls do it at maintenance hour).
```sql
delete from sm_pub_sub where processed='Y';
VACUUM FULL sm_pub_sub;
```

### What Next?
- GUI to show the state flow.
- WYSIWYG editor for state flow.
- GUI to show transaction status in the state flow.

## Example
#### Decision Flow:
![Example](example.PNG)
#### Sequence Diagram:  
![Example](example-sequence.PNG)

## Contributors
- Emerson Z CHEN