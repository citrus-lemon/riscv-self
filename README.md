# riscv-self

my own project by lihanyuan (lihanyuan1996@gmail.com)

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