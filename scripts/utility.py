from opcodes import codes

class ScalaWriter():
  def __init__(self, filename):
    self.fp = open(filename, 'w')
    self.fp.write("// auto generate by python scripts\n\n")
    self.fp.write("package rself\n\nimport chisel3._\n\n")

  def write(self, string):
    self.fp.write(string)

  def constants(self, identity):
    'const: (name, valuefmt, *fmts)'
    fp = self.fp
    def decorator(func):
      constlist = list(func())
      ml = max(map(lambda x:len(x[0]), constlist))
      fp.write("object {} {{\n".format(identity))
      for el in constlist:
        if not el:
          fp.write("\n")
        elif isinstance(el, str):
          fp.write("  // {}\n".format(el))
        else:
          fp.write(("  val {:%d} = {}\n" % ml).format(el[0], el[1].format(*el[2:])))
      fp.write("}\n\n")
    return decorator

  def namespace(self, identity):
    'create a namespace'
    fp = self.fp
    def decorator(func):
      codelist = func()
      fp.write("object {} {{\n".format(identity))
      for el in codelist:
        fp.write("  " + el + "\n")
      fp.write("}\n\n")
    return decorator

  @staticmethod
  def Seq(identity, cls='Map'):
    def decorator(func):
      cl = ['val {} = {}('.format(identity, cls)]
      tuplelist = list(func())
      ml = max(map(lambda x:len(x[0]), tuplelist))
      for el in tuplelist:
        cl.append(("  {:%d} -> {}," % ml).format(el[0], el[1].format(*el[2:])))
      cl[-1] = cl[-1][:-1] + ')'
      return lambda: cl
    return decorator

def getbits(code, hi, lo=None):
  if not lo: lo = hi
  return (code['const'] & (lambda x:(1<<(x[0]+1)) - (1<<x[1]))((hi, lo))) >> lo
