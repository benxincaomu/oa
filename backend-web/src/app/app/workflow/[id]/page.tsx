

export default async function WorkflowList({
  params,
}: {
  params: { id: string };
}) { 
    const {id} = await params;

    return (
        <div>
            {id}
        </div>
    )
}