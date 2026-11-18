import sys
from werkzeug.security import check_password_hash

if __name__ == "__main__":
    if len(sys.argv) == 3:
        pwhash = sys.argv[1]
        password = sys.argv[2]
        if check_password_hash(pwhash, password):
            print("true")
        else:
            print("false")
    else:
        print("error")
