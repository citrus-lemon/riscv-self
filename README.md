# riscv-self

my own project by lihanyuan (lihanyuan1996@gmail.com)

[在这里查看中文说明](https://github.com/WindProphet/riscv-self/wiki/riscv_self-%E9%A1%B9%E7%9B%AE%E4%BB%8B%E7%BB%8D)

## Install

```bash
git clone --recursive https://github.com/WindProphet/riscv-self
```

## Prerequisite

- java8
- sbt
- python3

## Usage

```bash
# build every part to verilog and firrtl
make compile

# test all
make test

# test only a part
make test-ALU

# clean built resource
make clean
```
