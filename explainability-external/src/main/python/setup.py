from setuptools import setup

setup(
    name="trustyaiexternal",
    version="0.1",
    description="TrustyAI external wrapper",
    author="Rui Vieira",
    author_email="rui@redhat.com",
    packages=["trustyaiexternal", "trustyaiexternal.algorithms", "trustyaiexternal.api"],
    install_requires=[
        "pandas~=1.5.3",
        "numpy",
        "jep==4.1.1",
        "urllib3==1.26.0",
        "requests",
        "aix360 [default,tsice,tslime,tssaliency] @ https://github.com/Trusted-AI/AIX360/archive/refs/heads/master.zip"
    ],
)
