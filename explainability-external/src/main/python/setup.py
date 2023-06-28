from setuptools import setup

setup(
    name="trustyaiexternal",
    version="0.1",
    description="TrustyAI external wrapper",
    author="Rui Vieira",
    author_email="rui@redhat.com",
    packages=["trustyaiexternal", "trustyaiexternal.algorithms", "trustyaiexternal.models"],
    install_requires=[
        "pandas<=1.4.3",  # For AIX360 compatibility
        "numpy",
        "jep==4.1.1",
        "grpcio",
        "protobuf==3.19.4",
    ],
)
