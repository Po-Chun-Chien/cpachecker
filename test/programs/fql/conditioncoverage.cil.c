/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 2 "conditioncoverage.c"
int foo(int x ) 
{ int a ;
  int tmp ;

  {
#line 3
  if (x > 2) {
#line 3
    if (x < 5) {
#line 3
      tmp = 1;
    } else {
#line 3
      tmp = 0;
    }
  } else {
#line 3
    tmp = 0;
  }
#line 3
  a = tmp;
#line 5
  if (a) {

  } else {

  }
#line 11
  return (0);
}
}
