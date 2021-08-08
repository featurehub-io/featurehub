def get_flag(key: string):
  return this.feature(key).getFlag()


def getString(key: string): string | undefined {
return this.feature(key).getString();
}

def getJson(key: string): string | undefined {
return this.feature(key).getRawJson();
}

def getNumber(key: string): number | undefined {
return this.feature(key).getNumber();
}

def isSet(key: string): boolean {
return this.feature(key).isSet();
}
