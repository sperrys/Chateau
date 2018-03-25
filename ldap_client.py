import ldap
import sys
import getpass

username = input("Username:")
pw = getpass.getpass("Password:")

l = ldap.initialize('ldaps://ldap.tufts.edu:636')
l.protocol_version = ldap.VERSION3

base = "ou=people,dc=tufts,dc=edu"
searchFilter = "uid="+username
searchScope = ldap.SCOPE_SUBTREE

# Search For Your DN based upon 
# your uuid
try:    
    res = l.search_s(base, searchScope, searchFilter, attrlist=['dn'])
    dn = res[0][0]
   
except ldap.LDAPError as e:
    print (e)
except Exception as e:
    print (e)

# Try Binding against your uuid with 
# your password.
try:
    res = l.simple_bind_s(dn, pw)

# Except for bad credentials
except ldap.INVALID_CREDENTIALS:
  print ("Your username or password is incorrect.")
  sys.exit(0)

# Catch other errors
except ldap.LDAPError as e:
  if type(e.message) == dict and e.message.has_key('desc'):
      print (e.message['desc'])
  else: 
      print (e)
  sys.exit(0)


print("Sucessful Login")
l.unbind_s()
